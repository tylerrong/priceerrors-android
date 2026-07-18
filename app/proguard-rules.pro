# Preserve the metadata used by Kotlin, Compose previews, serializers, and
# reflection-based SDKs without broadly disabling R8 optimization.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# Keep source/line information so obfuscated production crashes can be decoded
# with app/build/outputs/mapping/release/mapping.txt. File names are normalized
# so they do not reveal local build paths.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android framework callbacks referenced from XML are discovered by the Android
# Gradle plugin's generated rules. Add narrow SDK-specific rules below when a
# dependency's release notes require them; avoid blanket `-keep class **` rules.
