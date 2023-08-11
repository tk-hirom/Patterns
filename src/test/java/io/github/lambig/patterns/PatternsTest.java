package io.github.lambig.patterns;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static io.github.lambig.funcifextension.function.Functions.compositionOf;
import static io.github.lambig.funcifextension.function.Functions.sequenceOf;
import static io.github.lambig.patterns.Patterns.equalsTo;
import static io.github.lambig.patterns.Patterns.orElse;
import static io.github.lambig.patterns.Patterns.orElseThrow;
import static io.github.lambig.patterns.Patterns.patterns;
import static io.github.lambig.patterns.Patterns.then;
import static io.github.lambig.patterns.Patterns.thenApply;
import static io.github.lambig.patterns.Patterns.thenSupply;
import static io.github.lambig.patterns.Patterns.when;
import static io.github.lambig.patterns.Patterns.whenMatch;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatternsTest {

  @Nested
  class 設定と取得のテスト_get {
    @Test
    void 該当キーに対応する値または適用結果が取得できること() {
      //SetUp
      Patterns<Integer, String> target =
          patterns(
              when(equalsTo(3), then("b")),
              when(equalsTo(4), thenSupply(() -> "c")),
              when(i -> i > 0, thenApply(Object::toString)),
              when(i -> i < 0, thenApply(sequenceOf((UnaryOperator<Integer>) i -> i + 1, Number::longValue, Object::toString))),
              orElse(then("a")));

      //Exercise
      List<String> actual = Stream.of(-1, 0, 1, 2, 3, 4).map(target).collect(toList());
      //Verify
      assertThat(actual).containsExactly("0", "a", "1", "2", "b", "c");
    }

    @Test
    void 該当キーに対応する値がなければNoSuchPatternExceptionが取得できること() {
      //SetUp
      UnaryOperator<Integer> add1 = i -> i + 1;
      Patterns<Integer, String> target =
          patterns(
              when(equalsTo(3), then("b")),
              when(i -> i > 0, thenApply(Object::toString)),
              when(i -> i < 0, thenApply(compositionOf(Object::toString, i -> i, add1))));

      //Exercise
       var actual = assertThatThrownBy(() -> Stream.of(-1, 0, 1, 2, 3).map(target).collect(toList()));
      //Verify
      actual
              .isInstanceOf(Patterns.NoSuchPatternException.class)
              .extracting("message")
              .isEqualTo("for key: 0. To allow this pattern to return nullable value, consider using Patterns#getOptionally.");
    }

    @Test
    void orElseThrowで指定した例外がThrowできること() {
      //SetUp
      class ExpectedException extends RuntimeException{
      }
      Patterns<Integer, String> target =
              patterns(
                      when(equalsTo(3), then("b")),
                      orElseThrow(anything -> new ExpectedException()));

      //Exercise
      var actual = assertThatThrownBy(() -> Stream.of(-1, 0, 1, 2, 3).map(target).collect(toList()));
      //Verify
      actual.isInstanceOf(ExpectedException.class);
    }

    @Test
    void 該当キーに対応する値がnullであればNPEが送出されること() {
      //SetUp
      UnaryOperator<Integer> add1 = i -> i + 1;

      Patterns<Integer, String> target =
          patterns(
              when(equalsTo(3), then("b")),
              when(i -> i > 0, thenApply(Object::toString)),
              when(i -> i < 0, thenApply(compositionOf(Object::toString, i -> i, add1))),
              orElse(anything -> null));


      //Exercise
      var actual = assertThatThrownBy(() -> Stream.of(0).map(target).collect(toList()));

      //Verify
      actual
              .isInstanceOf(NullPointerException.class)
              .extracting("message")
              .isEqualTo("Pattern computed null result. To allow this pattern to return nullable value, consider using Patterns#getOptionally.");
    }

    @Test
    void 型パターンマッチのテスト() {
      //SetUp
      @RequiredArgsConstructor
      @Accessors(fluent = true)
      @Getter
      class A {
        final String value;
      }
      class B extends A {
        public B(String value) {
          super(value);
        }
      }
      class C extends A {
        public C(String value) {
          super(value);
        }

        public String say() {
          return "C here. I've got " + this.value() + ".";
        }
      }
      Patterns<A, String> target =
          patterns(
              whenMatch(B.class, thenApply(b -> "it's a B. value: " + b.value() + ".")),
              whenMatch(C.class, thenApply(C::say)),
              orElse(then("it's a plain A.")));

      //Exercise
      List<String> actual = Stream.of(new A("aaa"), new B("bbb"), new C("ccc")).map(target).collect(toList());
      //Verify
      assertThat(actual).containsExactly("it's a plain A.", "it's a B. value: bbb.", "C here. I've got ccc.");
    }
  }

  @Nested
  class 設定と取得のテスト_getOptinally {
    @Test
    void 該当キーに対応する値または適用結果が取得できること() {
      //SetUp
      Patterns<Integer, String> target =
              patterns(
                      when(equalsTo(3), then("b")),
                      when(equalsTo(4), thenSupply(() -> "c")),
                      when(i -> i > 0, thenApply(Object::toString)),
                      when(i -> i < 0, thenApply(sequenceOf((UnaryOperator<Integer>) i -> i + 1, Number::longValue, Object::toString))),
                      orElse(then("a")));

      //Exercise
      var actual = Stream.of(-1, 0, 1, 2, 3, 4).map(target.optional()).collect(toList());
      //Verify
      assertThat(actual).extracting(Optional::get).containsExactly("0", "a", "1", "2", "b", "c");
    }

    @Test
    void 該当キーに対応する値がなければemptyが取得できること() {
      //SetUp
      UnaryOperator<Integer> add1 = i -> i + 1;
      Patterns<Integer, String> target =
              patterns(
                      when(equalsTo(3), then("b")),
                      when(i -> i > 0, thenApply(Object::toString)),
                      when(i -> i < 0, thenApply(compositionOf(Object::toString, i -> i, add1))));

      //Exercise
      var actual = target.getOptionally(0);
      //Verify
      assertThat(actual).isEmpty();
    }

    @Test
    void orElseThrowで指定した例外がThrowできること() {
      //SetUp
      class ExpectedException extends RuntimeException{
      }
      Patterns<Integer, String> target =
              patterns(
                      when(equalsTo(3), then("b")),
                      orElseThrow(anything -> new ExpectedException()));

      //Exercise
      var actual = assertThatThrownBy(() -> Stream.of(-1, 0, 1, 2, 3).map(target.optional()).collect(toList()));
      //Verify
      actual.isInstanceOf(ExpectedException.class);
    }

    @Test
    void 型パターンマッチのテスト() {
      //SetUp
      @RequiredArgsConstructor
      @Accessors(fluent = true)
      @Getter
      class A {
        final String value;
      }
      class B extends A {
        public B(String value) {
          super(value);
        }
      }
      class C extends A {
        public C(String value) {
          super(value);
        }

        public String say() {
          return "C here. I've got " + this.value() + ".";
        }
      }
      Patterns<A, String> target =
              patterns(
                      whenMatch(B.class, thenApply(b -> "it's a B. value: " + b.value() + ".")),
                      whenMatch(C.class, thenApply(C::say)),
                      orElse(then("it's a plain A.")));

      //Exercise
      var actual = Stream.of(new A("aaa"), new B("bbb"), new C("ccc")).map(target.optional()).collect(toList());
      //Verify
      assertThat(actual).extracting(Optional::get).containsExactly("it's a plain A.", "it's a B. value: bbb.", "C here. I've got ccc.");
    }
  }
}