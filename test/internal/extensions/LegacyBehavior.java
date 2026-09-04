package internal.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/**
 * Marks an enabled characterization test whose expected behavior is known to be incorrect.
 *
 * <p>The required explanation should state what a correct implementation would do differently.
 * Remove the annotation when the behavior is intentionally corrected.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag("legacy-behavior")
public @interface LegacyBehavior {
  String value();
}
