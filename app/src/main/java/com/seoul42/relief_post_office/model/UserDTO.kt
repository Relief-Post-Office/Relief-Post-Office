package com.seoul42.relief_post_office.model

data class UserDTO(
    val photoUri: String?,
    val name: String?,
    val birth: String?, /* ex: 1997/09/22 */
    val tel: String?,
    var token: String?,
    val zoneCode: String?,
    val roadAddress: String?,
    val buildingName: String?,
    val detailAddress: String?,
    val gender: Boolean?, /* true(남), false(여) */
    val guardian: Boolean? /* true(보호자), false(피보호자) */
) {
    constructor(): this("","","","",
        "","","","","",
        false, false)
}
