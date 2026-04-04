# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.workoutlog.**$$serializer { *; }
-keepclassmembers class com.workoutlog.** { *** Companion; }
-keepclasseswithmembers class com.workoutlog.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# XMLBeans (required by poi-ooxml for XML serialization - missing this causes
# NoClassDefFoundError at runtime when writing .xlsx files)
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

# iText
-dontwarn com.itextpdf.**
-keep class com.itextpdf.** { *; }

# Suppress warnings for optional/unused transitive dependencies
-dontwarn aQute.bnd.annotation.baseline.BaselineIgnore
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Shape
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference
