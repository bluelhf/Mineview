module blue.lhf.mineview {
    exports blue.lhf.mineview to javafx.graphics;

    exports blue.lhf.mineview.main to javafx.graphics, javafx.fxml;

    opens blue.lhf.mineview.progress to javafx.fxml;
    opens blue.lhf.mineview.main to javafx.fxml;

    opens blue.lhf.mineview.model.piston_meta to
        com.fasterxml.jackson.databind,
        com.fasterxml.jackson.annotation,
        com.fasterxml.jackson.core;

    exports blue.lhf.mineview.model to com.fasterxml.jackson.databind;
    exports blue.lhf.mineview.model.procedure to com.fasterxml.jackson.databind;
    opens blue.lhf.mineview.model.logging to javafx.fxml;
    opens assets;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.jetbrains.annotations;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires proguard.base;
}