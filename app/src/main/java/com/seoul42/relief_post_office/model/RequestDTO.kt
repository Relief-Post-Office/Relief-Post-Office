package com.seoul42.relief_post_office.model

data class RequestDTO(
    val photoUri: String?,
    val name: String?,
    val birth: String?, /* ex: 1997/09/22 */
    val userId: String?
    ) {
        constructor(): this("","","","")
    }