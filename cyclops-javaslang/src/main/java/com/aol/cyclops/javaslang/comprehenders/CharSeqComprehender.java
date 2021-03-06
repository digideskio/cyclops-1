package com.aol.cyclops.javaslang.comprehenders;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.BaseStream;

import com.aol.cyclops.types.extensability.Comprehender;

import javaslang.collection.CharSeq;

public class CharSeqComprehender implements Comprehender<CharSeq> {

    @Override
    public Object map(CharSeq t, Function fn) {
        return t.map(s -> fn.apply(s));
    }

    @Override
    public Object executeflatMap(CharSeq t, Function fn) {
        return flatMap(t, input -> unwrapOtherMonadTypes(this, fn.apply(input)));
    }

    @Override
    public Object flatMap(CharSeq t, Function fn) {
        return t.flatMap(s -> (Iterable) fn.apply(s));
    }

    @Override
    public CharSeq of(Object o) {
        return CharSeq.of((Character) o);
    }

    @Override
    public CharSeq empty() {
        return CharSeq.empty();
    }

    @Override
    public Class getTargetClass() {
        return CharSeq.class;
    }

    static CharSeq unwrapOtherMonadTypes(Comprehender<CharSeq> comp, Object apply) {
        if (comp.instanceOfT(apply))
            return (CharSeq) apply;
        if (apply instanceof java.util.stream.Stream)
            return CharSeq.ofAll((Iterable<? extends Character>) ((java.util.stream.Stream) apply));
        if (apply instanceof Iterable)
            return CharSeq.ofAll((Iterable<? extends Character>) ((Iterable) apply));

        if (apply instanceof Collection) {
            return CharSeq.ofAll((Collection) apply);
        }
        final Object finalApply = apply;
        if (apply instanceof BaseStream) {
            return CharSeq.ofAll(() -> ((BaseStream) finalApply).iterator());

        }
        return Comprehender.unwrapOtherMonadTypes(comp, apply);

    }

    @Override
    public Object resolveForCrossTypeFlatMap(Comprehender comp, CharSeq apply) {
        return comp.fromIterator(apply.iterator());
    }

    @Override
    public CharSeq fromIterator(Iterator o) {
        return CharSeq.ofAll(() -> o);
    }

}
