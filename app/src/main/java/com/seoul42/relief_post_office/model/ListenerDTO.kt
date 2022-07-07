package com.seoul42.relief_post_office.model

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference

data class ListenerDTO(
    var reference : DatabaseReference,
    var listener : ChildEventListener
)