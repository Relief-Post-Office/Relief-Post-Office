package com.seoul42.relief_post_office.model

import java.io.Serializable

data class UserDTO(
    var photoUri: String,
    val name: String,
    val birth: String, /* ex: 1997/09/22 */
    val tel: String,
    var token: String,
    var zoneCode: String,
    var roadAddress: String,
    var buildingName: String,
    var detailAddress: String,
    val gender: Boolean, /* true(남), false(여) */
    val guardian: Boolean /* true(보호자), false(피보호자) */
) : Serializable {
    constructor(): this("","","","",
        "","","","","",
        false, false)
}