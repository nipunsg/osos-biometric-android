package com.lk.biometricrd.core.data.sources

import android.util.Log
import com.lk.biometricrd.core.data.DBResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BaseRepo {

    suspend fun <T : Any> safeDbCall(
        dbCall: suspend () -> T,
    ): DBResource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val result = dbCall.invoke()
                Log.d("BaseRepoSuccess", "safeDbCall: $result")
                DBResource.Success(result)
            } catch (exception: Exception) {
                Log.e("BaseRepoError", "safeDbCall: ${exception.message}")
                DBResource.Error(exception)
            }
        }
    }
}
