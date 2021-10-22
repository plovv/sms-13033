package com.plovv.sms13033

data class DataModel(var fullName: String, var address: String) {

    val fullNameEnabled: Boolean
        get() = fullName.isEmpty()

    val addressEnabled: Boolean
        get() = address.isEmpty()

}