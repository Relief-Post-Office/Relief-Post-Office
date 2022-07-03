package com.seoul42.relief_post_office.model

data class UserDTO(
    val photoUri: String?,
    val name: String?,
    val birth: String?, /* ex: 1997/09/22 */
    val tel: String?,
    val address: String?,
    var token: String?,
    val gender: Boolean?, /* true(남), false(여) */
    val guardian: Boolean? /* true(보호자), false(피보호자) */
) {
    constructor(): this("","","","",
        "","",false, false)
}
