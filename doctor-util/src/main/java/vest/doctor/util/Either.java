package vest.doctor.util;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public record Either<A, B>(A left, B right) {

    public static <A, B> Either<A, B> left(A left) {
        return new Either<>(left, null);
    }

    public static <A, B> Either<A, B> right(B right) {
        return new Either<>(null, right);
    }

    public Either<A, B> ifLeft(Consumer<A> action) {
        if (left != null) {
            action.accept(left);
        }
        return this;
    }

    public Either<A, B> ifRight(Consumer<B> action) {
        if (right != null) {
            action.accept(right);
        }
        return this;
    }

    public <O> Optional<O> ifLeft(Function<A, O> mapper) {
        return Optional.ofNullable(left).map(mapper);
    }

    public <O> Optional<O> ifRight(Function<B, O> mapper) {
        return Optional.ofNullable(right).map(mapper);
    }

    public <O> Optional<O> map(Function<A, O> mapIfLeft, Function<B, O> mapIfRight) {
        if (left != null) {
            return ifLeft(mapIfLeft);
        } else {
            return ifRight(mapIfRight);
        }
    }

    public Either<A, B> both(BiConsumer<A, B> action) {
        action.accept(left, right);
        return this;
    }

    public <O> O both(BiFunction<A, B, O> mapper) {
        return mapper.apply(left, right);
    }

    public boolean both() {
        return left != null && right != null;
    }

    public boolean none() {
        return left == null && right == null;
    }

    public Either<B, A> flip() {
        return new Either<>(right, left);
    }

    public A leftOrMap(Function<B, A> mapper) {
        if (left != null) {
            return left;
        } else {
            return mapper.apply(right);
        }
    }

    public B rightOrMap(Function<A, B> mapper) {
        if (right != null) {
            return right;
        } else {
            return mapper.apply(left);
        }
    }
}
