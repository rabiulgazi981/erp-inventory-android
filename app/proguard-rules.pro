# SQLCipher uses JNI and reflection.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# Room entities and DAOs are generated code; keep the entity fields Room reads by name.
-keep class com.bongshaltech.erpinventory.data.db.** { *; }
# Kotlin enums are looked up by name (valueOf) for saved settings and entry types.
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
