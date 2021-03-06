package move.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Actor {

  /**
   * Name of Actor.
   *
   * Defaults to FQN minus "actor."
   *
   * @return
   */
  String value() default "";
}
