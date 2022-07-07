package com.seoul42.relief_post_office.model

data class WardDTO(
    val connectedRegardIdList: ConnectedRegardsIdList,
    val resultIdList: ResultIdList,
    val requestedIdList: RequestedIdList,
    val connectedUserIdList: ConnectedUserIdList
) {
    data class ConnectedRegardsIdList(
        val connectedRegardIdList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    data class ResultIdList(
        val resultIdList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    data class RequestedIdList(
        val requestedIdList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
    data class ConnectedUserIdList(
        val connectedUserIdList : ArrayList<String>
    ) {
        constructor() : this(ArrayList<String>())
    }
}

//data class WardDTO(
//    val connectedRegardsList: ArrayList<String>,
//    val resultList: ArrayList<String>,
//    val requestedList: ArrayList<String>,
//    val connectedUserList: ArrayList<String>
//)