package com.pesu.pesudash.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.model.*
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ResultsTab { ISA, ESA }

data class IsaSemesterState(
    val isLoading: Boolean = true,
    val subjects: List<IsaSubjectView> = emptyList(),
    val error: String? = null
)

data class IsaSubjectView(
    val subjectCode: String,
    val subjectName: String,
    val credits: Double,
    val breakdown: List<IsaBreakdownItem>
)

data class EsaSemesterState(
    val isLoading: Boolean = true,
    val subjects: List<SubjectResultView> = emptyList(),
    val cgpaInfo: CgpaSemesterWise? = null,
    val sgpa: String? = null,
    val cgpa: String? = null,
    val totalCredits: String? = null,
    val earnedCredits: String? = null,
    val error: String? = null
)

data class ResultsUiState(
    val isLoading: Boolean = true,
    val activeTab: ResultsTab = ResultsTab.ISA,
    val isProvisional: Boolean = false,
    val error: String? = null,

    val isaTabSemesters: List<AttendanceSemester> = emptyList(),
    val isaSelectedIndex: Int = 0,
    val isaSemesterState: IsaSemesterState? = null,

    val esaTabSemesters: List<StudentSemester> = emptyList(),
    val esaSelectedIndex: Int = 0,
    val esaSemesterState: EsaSemesterState? = null
)

class ResultsViewModel(
    private val repository: PesuRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultsUiState())
    val state: StateFlow<ResultsUiState> = _state

    private var userId: String = ""
    private var usn: String    = ""
    private var loaded = false
    private var allAttSemesters: List<AttendanceSemester> = emptyList()

    fun load(userId: String, usn: String) {
        if (loaded) return
        loaded    = true
        this.userId = userId
        this.usn    = usn
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val attDeferred = async { repository.getAttendanceSemestersFull(userId) }
                val esaDeferred = async { repository.getResultSemesters(usn) }

                val attSemesters                  = attDeferred.await()
                val (esaSemesters, isProvisional) = esaDeferred.await()

                allAttSemesters = attSemesters.sortedByDescending { it.batchClassOrder }

                val completedEsa = esaSemesters
                    .filter { it.classStatus == 1 || it.sgpa != null }
                    .sortedByDescending { it.batchClassOrder }

                _state.value = _state.value.copy(
                    isLoading        = false,
                    isProvisional    = isProvisional,
                    isaTabSemesters  = allAttSemesters,
                    esaTabSemesters  = completedEsa,
                    isaSelectedIndex = 0,
                    esaSelectedIndex = 0
                )

                if (allAttSemesters.isNotEmpty()) loadIsaDetail(allAttSemesters[0])
                if (completedEsa.isNotEmpty()) loadEsaDetail(completedEsa[0])

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = e.message ?: "Failed to load results"
                )
            }
        }
    }

    fun switchTab(tab: ResultsTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun selectIsaSemester(index: Int) {
        val semesters = _state.value.isaTabSemesters
        if (index < 0 || index >= semesters.size) return
        _state.value = _state.value.copy(isaSelectedIndex = index)
        loadIsaDetail(semesters[index])
    }

    fun selectEsaSemester(index: Int) {
        val semesters = _state.value.esaTabSemesters
        if (index < 0 || index >= semesters.size) return
        _state.value = _state.value.copy(esaSelectedIndex = index)
        loadEsaDetail(semesters[index])
    }

    fun retryIsa() {
        val semesters = _state.value.isaTabSemesters
        val index     = _state.value.isaSelectedIndex
        if (semesters.isEmpty() || index >= semesters.size) return
        loadIsaDetail(semesters[index])
    }

    fun retryEsa() {
        val semesters = _state.value.esaTabSemesters
        val index     = _state.value.esaSelectedIndex
        if (semesters.isEmpty() || index >= semesters.size) return
        loadEsaDetail(semesters[index])
    }

    fun reload() {
        loaded = false
        _state.value = ResultsUiState()
        load(userId, usn)
    }

    private fun loadIsaDetail(semester: AttendanceSemester) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isaSemesterState = IsaSemesterState(isLoading = true)
            )
            try {
                val isaMarks = repository.getIsaMarks(
                    userId              = userId,
                    batchClassId        = semester.batchClassId,
                    classBatchSectionId = semester.classBatchSectionId
                )

                if (isaMarks.isEmpty()) {
                    _state.value = _state.value.copy(
                        isaSemesterState = IsaSemesterState(
                            isLoading = false,
                            error     = "No ISA marks available for this semester"
                        )
                    )
                    return@launch
                }

                val subjectMap     = mutableMapOf<String, MutableList<IsaBreakdownItem>>()
                val subjectNames   = mutableMapOf<String, String>()
                val subjectCredits = mutableMapOf<String, Double>()

                for ((_, entries) in isaMarks) {
                    for (entry in entries) {
                        val code = entry.subjectCode
                        subjectNames[code]   = entry.subjectName
                        subjectCredits[code] = entry.credits
                        val list = subjectMap.getOrPut(code) { mutableListOf() }
                        list.add(
                            IsaBreakdownItem(
                                assessmentName = entry.isaMaster?.trim() ?: "Unknown",
                                marks          = entry.marks,
                                maxMarks       = entry.maxIsaMarks,
                                cutoff         = entry.cutoffMarks,
                                orderBy        = entry.orderBy ?: 0
                            )
                        )
                    }
                }

                val subjects = subjectMap.entries.map { (code, items) ->
                    items.sortBy { it.orderBy }
                    IsaSubjectView(
                        subjectCode = code,
                        subjectName = subjectNames[code] ?: code,
                        credits     = subjectCredits[code] ?: 0.0,
                        breakdown   = items
                    )
                }.sortedBy { it.subjectCode }

                _state.value = _state.value.copy(
                    isaSemesterState = IsaSemesterState(
                        isLoading = false,
                        subjects  = subjects
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isaSemesterState = IsaSemesterState(
                        isLoading = false,
                        error     = e.message ?: "Failed to load ISA marks"
                    )
                )
            }
        }
    }

    private fun loadEsaDetail(semester: StudentSemester) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                esaSemesterState = EsaSemesterState(isLoading = true)
            )
            try {
                val isFinalised = semester.classStatus == 1

                val gradesDeferred = async {
                    repository.getSemesterGrades(
                        batchClassId = semester.batchClassId,
                        classesId    = semester.classesId,
                        userId       = userId,
                        usn          = usn,
                        className    = semester.className,
                        isFinalised  = isFinalised
                    )
                }

                val isaDeferred = async {
                    val matched = allAttSemesters.find {
                        it.batchClassId == semester.batchClassId
                    }
                    if (matched != null) {
                        try {
                            repository.getIsaMarks(
                                userId              = userId,
                                batchClassId        = matched.batchClassId,
                                classBatchSectionId = matched.classBatchSectionId
                            )
                        } catch (_: Exception) { emptyMap<String, List<IsaMarkEntry>>() }
                    } else {
                        emptyMap()
                    }
                }

                val (grades, cgpaInfo) = gradesDeferred.await()
                val isaMarks           = isaDeferred.await()

                val isaBySubject = mutableMapOf<String, MutableList<IsaBreakdownItem>>()
                for ((_, entries) in isaMarks) {
                    for (entry in entries) {
                        val list = isaBySubject.getOrPut(entry.subjectCode) { mutableListOf() }
                        list.add(
                            IsaBreakdownItem(
                                assessmentName = entry.isaMaster?.trim() ?: "Unknown",
                                marks          = entry.marks,
                                maxMarks       = entry.maxIsaMarks,
                                cutoff         = entry.cutoffMarks,
                                orderBy        = entry.orderBy ?: 0
                            )
                        )
                    }
                }
                isaBySubject.values.forEach { it.sortBy { item -> item.orderBy } }

                val subjects = grades.map { grade ->
                    SubjectResultView(
                        subjectCode   = grade.subjectCode,
                        subjectName   = grade.subjectName,
                        credits       = grade.credits,
                        grade         = grade.grade,
                        earnedCredits = grade.earnedCredit,
                        isaBreakdown  = isaBySubject[grade.subjectCode] ?: emptyList()
                    )
                }

                _state.value = _state.value.copy(
                    esaSemesterState = EsaSemesterState(
                        isLoading     = false,
                        subjects      = subjects,
                        cgpaInfo      = cgpaInfo,
                        sgpa          = cgpaInfo?.sgpa ?: semester.sgpa,
                        cgpa          = cgpaInfo?.cgpa ?: semester.cgpa,
                        totalCredits  = cgpaInfo?.credits,
                        earnedCredits = cgpaInfo?.earnedCredits
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    esaSemesterState = EsaSemesterState(
                        isLoading = false,
                        error     = e.message ?: "Failed to load ESA results"
                    )
                )
            }
        }
    }

    class Factory(
        private val repository: PesuRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResultsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ResultsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}