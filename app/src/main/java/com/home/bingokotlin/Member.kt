package com.home.bingokotlin

data class Member(var avatarId : Int,
                  var displayName : String,
                  var nickname : String?,var uid : String){
        constructor() : this(0,"",null,"")
}