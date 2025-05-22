package com.dfx0.modbustool.model.enums

enum class VarType(val displayName: String) {
    INT16("INT16"),
    INT32("INT32"),
    UINT16("UINT16"),
    UINT32("UINT32"),
    REAL("REAL"),
    BOOL("按钮"),
    JoyBOOL("点动按钮");

    override fun toString(): String {
        return displayName
    }
}
