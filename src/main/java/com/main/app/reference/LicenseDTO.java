package com.main.app.reference;


public record LicenseDTO(
        String key,
        String name,
        String desc
) {

    public static LicenseDTO from(License entity) {
        return new LicenseDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDesc()
        );
    }
}
