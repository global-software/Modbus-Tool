package com.dfx0.modbustool.model.converts;


import androidx.room.TypeConverter;
import com.dfx0.modbustool.model.enums.VarType;

public class EnumConvert {

    @TypeConverter
    public String fromVarType(VarType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public VarType toVarType(String value) {
        if (value == null) {
            return null;
        }
        return VarType.valueOf(value);
    }
}

