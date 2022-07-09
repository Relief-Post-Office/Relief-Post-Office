package com.seoul42.relief_post_office.model

import java.io.Serializable

data class WardDTO(
    val connectedSafetyIdList: ConnectedSafetyIdData?,
    val resultIdList: ResultIdData?,
    val requestedUserIdList: RequestedUserIdData?,
    val connectedUserIdList: ConnectedUserIdData?
) : Serializable {
    data class ConnectedSafetyIdData(
        val connectedSafetyIdList : ArrayList<String>
    ) : Serializable {
        constructor() : this(ArrayList<String>())
    }
    data class ResultIdData(
        val resultIdList : ArrayList<String>
    ) : Serializable {
        constructor() : this(ArrayList<String>())
    }
    data class RequestedUserIdData(
        val requestedUserIdList : ArrayList<String>
    ) : Serializable {
        constructor() : this(ArrayList<String>())
    }
    data class ConnectedUserIdData(
        val connectedUserIdList : ArrayList<String>
    ) : Serializable {
        constructor() : this(ArrayList<String>())
    }
    constructor() : this(null, null, null, null)
}

//data class WardDTO(
//    val connectedSafetyIdList: ArrayList<String>,
//    val resultIdList: ArrayList<String>,
//    val requestedUserIdList: ArrayList<String>,
//    val connectedUserIdList: ArrayList<String>
//) {
//    constructor() : this(arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf())
//}