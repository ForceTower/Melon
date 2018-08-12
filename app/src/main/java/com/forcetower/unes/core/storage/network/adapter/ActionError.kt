package com.forcetower.unes.core.storage.network.adapter

import java.util.HashMap

/**
 * Created by João Paulo on 29/04/2018.
 */
class ActionError(var message: String?) {
    var code: Int = 0
    var isError: Boolean = false
    var errors: HashMap<String, List<String>>? = null
}
