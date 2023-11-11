package com.funny.translation.translate.ui.long_text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funny.translation.translate.database.LongTextTransTask
import com.funny.translation.translate.database.appDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LongTextTransListViewModel: ViewModel() {
    private val dao = appDB.longTextTransDao

    val taskList = appDB.longTextTransDao.getAll()

    fun deleteTask(task: LongTextTransTask) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(task)
        }
    }
}