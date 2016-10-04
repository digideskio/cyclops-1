package com.aol.cyclops.reactor.collections.extensions.standard;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.data.collections.extensions.standard.DequeX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.reactor.FluxUtils;
import com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX;
import com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollection;

import lombok.Getter;
import reactor.core.publisher.Flux;

public class LazyDequeX<T> extends AbstractFluentCollectionX<T> implements DequeX<T> {
    private final  LazyFluentCollection<T,List<T>> lazy;
    @Getter
    private final Collector<T,?,List<T>> collector;
    
    
    public static <T> LazyDequeX<T> fromStreamS(Stream<T> stream){
        return new LazyDequeX<T>(Flux.from(ReactiveSeq.fromStream(stream)));
    }
    /**
     * Create a LazyListX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyDequeX<Integer> range(int start, int end) {
        return fromStreamS(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyListX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyDequeX<Long> rangeLong(long start, long end) {
        return fromStreamS(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    static <U, T> LazyDequeX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStreamS(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyListX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyDequeX<T> generate(long limit, Supplier<T> s) {

        return fromStreamS(ReactiveSeq.generate(s)
                          .limit(limit));
    }

    /**
     * Create a LazyListX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyDequeX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStreamS(ReactiveSeq.iterate(seed, f)
                          .limit(limit));
    }


    /**
     * @return A collector that generates a LazyListX
     */
    static <T> Collector<T, ?, LazyDequeX<T>> lazyListXCollector() {
        return Collectors.toCollection(() -> LazyDequeX.of());
    }

   

    /**
     * @return An empty LazyListX
     */
    public static <T> LazyDequeX<T> empty() {
        return fromIterable((List<T>) ListX.<T>defaultCollector().supplier()
                                                        .get());
    }

    /**
     * Create a LazyListX from the specified values
     * <pre>
     * {@code 
     *     ListX<Integer> lazy = LazyListX.of(1,2,3,4,5);
     *     
     *     //lazily map List
     *     ListX<String> mapped = lazy.map(i->"mapped " +i); 
     *     
     *     String value = mapped.get(0); //transformation triggered now
     * }
     * </pre>
     * 
     * @param values To populate LazyListX with
     * @return LazyListX
     */
    @SafeVarargs
    public static <T> LazyDequeX<T> of(T... values) {
        List<T> res = (List<T>) ListX.<T>defaultCollector().supplier()
                                                  .get();
        for (T v : values)
            res.add(v);
        return fromIterable(res);
    }

    /**
     * Construct a LazyListX with a single value
     * <pre>
     * {@code 
     *    ListX<Integer> lazy = LazyListX.singleton(5);
     *    
     * }
     * </pre>
     * 
     * 
     * @param value To populate LazyListX with
     * @return LazyListX with a single value
     */
    public static <T> LazyDequeX<T> singleton(T value) {
        return LazyDequeX.<T> of(value);
    }

    /**
     * Construct a LazyListX from an Publisher
     * 
     * @param publisher
     *            to construct LazyListX from
     * @return ListX
     */
    public static <T> LazyDequeX<T> fromPublisher(Publisher<? extends T> publisher) {
        return fromStreamS(ReactiveSeq.fromPublisher((Publisher<T>) publisher));
    }

    /**
     * Construct LazyListX from an Iterable
     * 
     * @param it to construct LazyListX from
     * @return LazyListX from Iterable
     */
    public static <T> LazyDequeX<T> fromIterable(Iterable<T> it) {
        return fromIterable(ListX.<T>defaultCollector(), it);
    }

    /**
     * Construct a LazyListX from an Iterable, using the specified Collector.
     * 
     * @param collector To generate Lists from, this can be used to create mutable vs immutable Lists (for example), or control List type (ArrayList, LinkedList)
     * @param it Iterable to construct LazyListX from
     * @return Newly constructed LazyListX
     */
    public static <T> LazyDequeX<T> fromIterable(Collector<T, ?, List<T>> collector, Iterable<T> it) {
        if (it instanceof LazyDequeX)
            return (LazyDequeX<T>) it;
       
        if (it instanceof List)
            return new LazyDequeX<T>(
                                    (List<T>) it, collector);
        return new LazyDequeX<T>(
                                Flux.fromIterable(it),
                                collector);
    }
    private LazyDequeX(List<T> list,Collector<T,?,List<T>> collector){
        this.lazy = new LazyCollection<>(list,null,collector);
        this.collector=  collector;
    }
    
    private LazyDequeX(List<T> list){
        
        this.collector = ListX.defaultCollector();
        this.lazy = new LazyCollection<T,List<T>>(list,null,collector);
    }
    private LazyDequeX(Flux<T> stream,Collector<T,?,List<T>> collector){
        
        this.collector = collector;
        this.lazy = new LazyCollection<>(null,stream,collector);
    }
    private LazyDequeX(Flux<T> stream){
        
        this.collector = ListX.defaultCollector();
        this.lazy = new LazyCollection<>(null,stream,collector);
    }
    private LazyDequeX(){
        this.collector = ListX.defaultCollector();
        this.lazy = new LazyCollection<>((List)this.collector.supplier().get(),null,collector);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        getList().forEach(action);
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return getList().iterator();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        return getList().size();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object e) {
        return getList().contains(e);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return getList().equals(o);
    }



    /* (non-Javadoc)
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return getList().isEmpty();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getList().hashCode();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        return getList().toArray();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return getList().removeAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return getList().toArray(a);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(T e) {
        return getList().add(e);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        return getList().remove(o);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return getList().containsAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        return getList().addAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return getList().retainAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#clear()
     */
    @Override
    public void clear() {
        getList().clear();
    }

    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getList().toString();
    }

    /* (non-Javadoc)
     * @see org.jooq.lambda.Collectable#collect(java.util.stream.Collector)
     */
    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    /* (non-Javadoc)
     * @see org.jooq.lambda.Collectable#count()
     */
    @Override
    public long count() {
        return this.size();
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#removeIf(java.util.function.Predicate)
     */
    @Override
    public  boolean removeIf(Predicate<? super T> filter) {
        return getList().removeIf(filter);
    }
    
    
   
    /* (non-Javadoc)
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public  Stream<T> parallelStream() {
        return getList().parallelStream();
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#spliterator()
     */
    @Override
    public Spliterator<T> spliterator() {
        return getList().spliterator();
    }
   
    private List<T> getList() {
        return lazy.get();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#stream(reactor.core.publisher.Flux)
     */
    @Override
    public <X> LazyDequeX<X> stream(Flux<X> stream){
        return new LazyDequeX<X>(stream);
    }
   
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.persistent.PBagX#stream()
     */
    @Override
    public ReactiveSeq<T> stream() {
        return ReactiveSeq.fromStream(lazy.get().stream());
    }
    @Override
    public Flux<T> streamInternal() {
        return lazy.stream();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#combine(java.util.function.BiPredicate, java.util.function.BinaryOperator)
     */
    @Override
    public LazyDequeX<T> combine(BiPredicate<? super T, ? super T> predicate, BinaryOperator<T> op) {
       
        return (LazyDequeX<T>)super.combine(predicate, op);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#reverse()
     */
    @Override
    public LazyDequeX<T> reverse() {
       
        return(LazyDequeX<T>)super.reverse();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#filter(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> filter(Predicate<? super T> pred) {
       
        return (LazyDequeX<T>)super.filter(pred);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#map(java.util.function.Function)
     */
    @Override
    public <R> LazyDequeX<R> map(Function<? super T, ? extends R> mapper) {
       
        return (LazyDequeX<R>)super.map(mapper);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#flatMap(java.util.function.Function)
     */
    @Override
    public <R> LazyDequeX<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
       return (LazyDequeX<R>)super.flatMap(mapper);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#limit(long)
     */
    @Override
    public LazyDequeX<T> limit(long num) {
       return (LazyDequeX<T>)super.limit(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#skip(long)
     */
    @Override
    public LazyDequeX<T> skip(long num) {
       return (LazyDequeX<T>)super.skip(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#takeRight(int)
     */
    @Override
    public LazyDequeX<T> takeRight(int num) {
       return (LazyDequeX<T>)super.takeRight(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#dropRight(int)
     */
    @Override
    public LazyDequeX<T> dropRight(int num) {
       return (LazyDequeX<T>)super.dropRight(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#takeWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> takeWhile(Predicate<? super T> p) {
       return (LazyDequeX<T>)super.takeWhile(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#dropWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> dropWhile(Predicate<? super T> p) {
       return (LazyDequeX<T>)super.dropWhile(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#takeUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> takeUntil(Predicate<? super T> p) {
       return (LazyDequeX<T>)super.takeUntil(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#dropUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> dropUntil(Predicate<? super T> p) {
       return(LazyDequeX<T>)super.dropUntil(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#trampoline(java.util.function.Function)
     */
    @Override
    public <R> LazyDequeX<R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
       return (LazyDequeX<R>)super.trampoline(mapper);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#slice(long, long)
     */
    @Override
    public LazyDequeX<T> slice(long from, long to) {
       return (LazyDequeX<T>)super.slice(from, to);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#grouped(int)
     */
    @Override
    public LazyDequeX<ListX<T>> grouped(int groupSize) {
       
        return (LazyDequeX<ListX<T>>)super.grouped(groupSize);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#grouped(java.util.function.Function, java.util.stream.Collector)
     */
    @Override
    public <K, A, D> LazyDequeX<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {
       
        return (LazyDequeX)super.grouped(classifier, downstream);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#grouped(java.util.function.Function)
     */
    @Override
    public <K> LazyDequeX<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier) {
       
        return (LazyDequeX)super.grouped(classifier);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zip(java.lang.Iterable)
     */
    @Override
    public <U> LazyDequeX<Tuple2<T, U>> zip(Iterable<? extends U> other) {
       
        return (LazyDequeX)super.zip(other);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyDequeX<R> zip(Iterable<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
       
        return (LazyDequeX<R>)super.zip(other, zipper);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#sliding(int)
     */
    @Override
    public LazyDequeX<ListX<T>> sliding(int windowSize) {
       
        return (LazyDequeX<ListX<T>>)super.sliding(windowSize);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#sliding(int, int)
     */
    @Override
    public LazyDequeX<ListX<T>> sliding(int windowSize, int increment) {
       
        return (LazyDequeX<ListX<T>>)super.sliding(windowSize, increment);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#scanLeft(com.aol.cyclops.Monoid)
     */
    @Override
    public LazyDequeX<T> scanLeft(Monoid<T> monoid) {
       
        return (LazyDequeX<T>)super.scanLeft(monoid);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#scanLeft(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <U> LazyDequeX<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {
       
        return (LazyDequeX<U>) super.scanLeft(seed, function);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#scanRight(com.aol.cyclops.Monoid)
     */
    @Override
    public LazyDequeX<T> scanRight(Monoid<T> monoid) {
       
        return (LazyDequeX<T>)super.scanRight(monoid);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#scanRight(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <U> LazyDequeX<U> scanRight(U identity, BiFunction<? super T, ? super U, ? extends U> combiner) {
       
        return (LazyDequeX<U>)super.scanRight(identity, combiner);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#sorted(java.util.function.Function)
     */
    @Override
    public <U extends Comparable<? super U>> LazyDequeX<T> sorted(Function<? super T, ? extends U> function) {
       
        return (LazyDequeX<T>)super.sorted(function);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#plus(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> plus(T e) {
       
        return (LazyDequeX<T>)super.plus(e);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#plusAll(java.util.Collection)
     */
    @Override
    public LazyDequeX<T> plusAll(Collection<? extends T> list) {
       
        return (LazyDequeX<T>)super.plusAll(list);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#minus(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> minus(Object e) {
       
        return (LazyDequeX<T>)super.minus(e);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#minusAll(java.util.Collection)
     */
    @Override
    public LazyDequeX<T> minusAll(Collection<?> list) {
       
        return (LazyDequeX<T>)super.minusAll(list);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#plusLazy(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> plusLazy(T e) {
       
        return (LazyDequeX<T>)super.plus(e);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#plusAllLazy(java.util.Collection)
     */
    @Override
    public LazyDequeX<T> plusAllLazy(Collection<? extends T> list) {
       
        return (LazyDequeX<T>)super.plusAll(list);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#minusLazy(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> minusLazy(Object e) {
       
        return (LazyDequeX<T>)super.minus(e);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#minusAllLazy(java.util.Collection)
     */
    @Override
    public LazyDequeX<T> minusAllLazy(Collection<?> list) {
       
        return (LazyDequeX<T>)super.minusAll(list);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#cycle(int)
     */
    @Override
    public LazyDequeX<T> cycle(int times) {
       
        return (LazyDequeX<T>)super.cycle(times);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#cycle(com.aol.cyclops.Monoid, int)
     */
    @Override
    public LazyDequeX<T> cycle(Monoid<T> m, int times) {
       
        return (LazyDequeX<T>)super.cycle(m, times);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#cycleWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> cycleWhile(Predicate<? super T> predicate) {
       
        return (LazyDequeX<T>)super.cycleWhile(predicate);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#cycleUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> cycleUntil(Predicate<? super T> predicate) {
       
        return (LazyDequeX<T>)super.cycleUntil(predicate);
    }
    
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zip(org.jooq.lambda.Seq)
     */
    @Override
    public <U> LazyDequeX<Tuple2<T, U>> zip(Seq<? extends U> other) {
       
        return (LazyDequeX)super.zip(other);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zip3(java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    public <S, U> LazyDequeX<Tuple3<T, S, U>> zip3(Stream<? extends S> second, Stream<? extends U> third) {
       
        return (LazyDequeX)super.zip3(second, third);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zip4(java.util.stream.Stream, java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    public <T2, T3, T4> LazyDequeX<Tuple4<T, T2, T3, T4>> zip4(Stream<? extends T2> second, Stream<? extends T3> third,
            Stream<? extends T4> fourth) {
       
        return (LazyDequeX)super.zip4(second, third, fourth);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#zipWithIndex()
     */
    @Override
    public LazyDequeX<Tuple2<T, Long>> zipWithIndex() {
       
        return (LazyDequeX<Tuple2<T, Long>>)super.zipWithIndex();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#distinct()
     */
    @Override
    public LazyDequeX<T> distinct() {
       
        return (LazyDequeX<T>)super.distinct();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#sorted()
     */
    @Override
    public LazyDequeX<T> sorted() {
       
        return (LazyDequeX<T>)super.sorted();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#sorted(java.util.Comparator)
     */
    @Override
    public LazyDequeX<T> sorted(Comparator<? super T> c) {
       
        return (LazyDequeX<T>)super.sorted(c);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#skipWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> skipWhile(Predicate<? super T> p) {
       
        return (LazyDequeX<T>)super.skipWhile(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#skipUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> skipUntil(Predicate<? super T> p) {
       
        return (LazyDequeX<T>)super.skipUntil(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#limitWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> limitWhile(Predicate<? super T> p) {
       
        return (LazyDequeX<T>)super.limitWhile(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#limitUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> limitUntil(Predicate<? super T> p) {
       
        return (LazyDequeX<T>)super.limitUntil(p);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#intersperse(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> intersperse(T value) {
       
        return (LazyDequeX<T>)super.intersperse(value);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#shuffle()
     */
    @Override
    public LazyDequeX<T> shuffle() {
       
        return (LazyDequeX<T>)super.shuffle();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#skipLast(int)
     */
    @Override
    public LazyDequeX<T> skipLast(int num) {
       
        return (LazyDequeX<T>)super.skipLast(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#limitLast(int)
     */
    @Override
    public LazyDequeX<T> limitLast(int num) {
       
        return (LazyDequeX<T>)super.limitLast(num);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#onEmpty(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> onEmpty(T value) {
       
        return (LazyDequeX<T>)super.onEmpty(value);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    public LazyDequeX<T> onEmptyGet(Supplier<? extends T> supplier) {
       
        return (LazyDequeX<T>)super.onEmptyGet(supplier);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    public <X extends Throwable> LazyDequeX<T> onEmptyThrow(Supplier<? extends X> supplier) {
       
        return (LazyDequeX<T>)super.onEmptyThrow(supplier);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#shuffle(java.util.Random)
     */
    @Override
    public LazyDequeX<T> shuffle(Random random) {
       
        return (LazyDequeX<T>)super.shuffle(random);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#ofType(java.lang.Class)
     */
    @Override
    public <U> LazyDequeX<U> ofType(Class<? extends U> type) {
       
        return (LazyDequeX<U>)super.ofType(type);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#filterNot(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<T> filterNot(Predicate<? super T> fn) {
       
        return (LazyDequeX<T>)super.filterNot(fn);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#notNull()
     */
    @Override
    public LazyDequeX<T> notNull() {
       
        return (LazyDequeX<T>)super.notNull();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#removeAll(java.util.stream.Stream)
     */
    @Override
    public LazyDequeX<T> removeAll(Stream<? extends T> stream) {
       
        return (LazyDequeX<T>)(super.removeAll(stream));
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#removeAll(org.jooq.lambda.Seq)
     */
    @Override
    public LazyDequeX<T> removeAll(Seq<? extends T> stream) {
       
        return (LazyDequeX<T>)super.removeAll(stream);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#removeAll(java.lang.Iterable)
     */
    @Override
    public LazyDequeX<T> removeAll(Iterable<? extends T> it) {
       
        return (LazyDequeX<T>)super.removeAll(it);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#removeAll(java.lang.Object[])
     */
    @Override
    public LazyDequeX<T> removeAll(T... values) {
       
        return (LazyDequeX<T>)super.removeAll(values);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#retainAll(java.lang.Iterable)
     */
    @Override
    public LazyDequeX<T> retainAll(Iterable<? extends T> it) {
       
        return (LazyDequeX<T>)super.retainAll(it);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#retainAll(java.util.stream.Stream)
     */
    @Override
    public LazyDequeX<T> retainAll(Stream<? extends T> stream) {
       
        return (LazyDequeX<T>)super.retainAll(stream);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#retainAll(org.jooq.lambda.Seq)
     */
    @Override
    public LazyDequeX<T> retainAll(Seq<? extends T> stream) {
       
        return (LazyDequeX<T>)super.retainAll(stream);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#retainAll(java.lang.Object[])
     */
    @Override
    public LazyDequeX<T> retainAll(T... values) {
       
        return (LazyDequeX<T>)super.retainAll(values);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#cast(java.lang.Class)
     */
    @Override
    public <U> LazyDequeX<U> cast(Class<? extends U> type) {
       
        return (LazyDequeX<U>)super.cast(type);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    public <R> LazyDequeX<R> patternMatch(Function<CheckValue1<T, R>, CheckValue1<T, R>> case1,
            Supplier<? extends R> otherwise) {
       
        return (LazyDequeX<R>)super.patternMatch(case1, otherwise);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#permutations()
     */
    @Override
    public LazyDequeX<ReactiveSeq<T>> permutations() {
       
        return (LazyDequeX<ReactiveSeq<T>>)super.permutations();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#combinations(int)
     */
    @Override
    public LazyDequeX<ReactiveSeq<T>> combinations(int size) {
       
        return (LazyDequeX<ReactiveSeq<T>>)super.combinations(size);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#combinations()
     */
    @Override
    public LazyDequeX<ReactiveSeq<T>> combinations() {
       
        return (LazyDequeX<ReactiveSeq<T>>)super.combinations();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#grouped(int, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyDequeX<C> grouped(int size, Supplier<C> supplier) {
       
        return (LazyDequeX<C>)super.grouped(size, supplier);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#groupedUntil(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<ListX<T>> groupedUntil(Predicate<? super T> predicate) {
       
        return (LazyDequeX<ListX<T>>)super.groupedUntil(predicate);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#groupedWhile(java.util.function.Predicate)
     */
    @Override
    public LazyDequeX<ListX<T>> groupedWhile(Predicate<? super T> predicate) {
       
        return (LazyDequeX<ListX<T>>)super.groupedWhile(predicate);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#groupedWhile(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyDequeX<C> groupedWhile(Predicate<? super T> predicate,
            Supplier<C> factory) {
        
        return (LazyDequeX<C>)super.groupedWhile(predicate, factory);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#groupedUntil(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    public <C extends Collection<? super T>> LazyDequeX<C> groupedUntil(Predicate<? super T> predicate,
            Supplier<C> factory) {
        
        return (LazyDequeX<C>)super.groupedUntil(predicate, factory);
    }
   
    /** ListX methods **/

    /* Makes a defensive copy of this ListX replacing the value at i with the specified element
     *  (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableSequenceX#with(int, java.lang.Object)
     */
    public LazyDequeX<T> with(int i,T element){
        return stream( FluxUtils.insertAt(FluxUtils.deleteBetween(streamInternal(),i, i+1),i,element)) ;
    }
    
    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX#unit(java.util.Collection)
     */
    @Override
    public <R> LazyDequeX<R> unit(Collection<R> col){
        return LazyDequeX.fromIterable(col);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Unit#unit(java.lang.Object)
     */
    @Override
    public  <R> LazyDequeX<R> unit(R value){
        return LazyDequeX.singleton(value);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#unitIterator(java.util.Iterator)
     */
    @Override
    public <R> LazyDequeX<R> unitIterator(Iterator<R> it){
        return LazyDequeX.fromIterable(()->it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX#plusInOrder(java.lang.Object)
     */
    @Override
    public LazyDequeX<T> plusInOrder(T e) {
        
        return (LazyDequeX<T>)super.plusInOrder(e);
    }
    

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#from(java.util.Collection)
     */
    @Override
    public <T1> LazyDequeX<T1> from(Collection<T1> c) {
        if(c instanceof List)
            return new LazyDequeX<T1>((List)c,(Collector)collector);
      return new LazyDequeX<T1>((List)c.stream().collect(Collectors.toList()),(Collector)this.collector);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#groupedStatefullyUntil(java.util.function.BiPredicate)
     */
    @Override
    public LazyDequeX<ListX<T>> groupedStatefullyUntil(BiPredicate<ListX<? super T>, ? super T> predicate) {
        
        return (LazyDequeX<ListX<T>>)super.groupedStatefullyUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#peek(java.util.function.Consumer)
     */
    @Override
    public LazyDequeX<T> peek(Consumer<? super T> c) {
        
        return (LazyDequeX)super.peek(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#zip(org.jooq.lambda.Seq, java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyDequeX<R> zip(Seq<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper) {
        
        return (LazyDequeX<R>)super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#zip(java.util.stream.Stream, java.util.function.BiFunction)
     */
    @Override
    public <U, R> LazyDequeX<R> zip(Stream<? extends U> other,
            BiFunction<? super T, ? super U, ? extends R> zipper) {
      
        return (LazyDequeX<R>)super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#zip(java.util.stream.Stream)
     */
    @Override
    public <U> LazyDequeX<Tuple2<T, U>> zip(Stream<? extends U> other) {
        
        return (LazyDequeX)super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.reactor.collections.extensions.base.AbstractFluentCollectionX#zip(java.util.function.BiFunction, org.reactivestreams.Publisher)
     */
    @Override
    public <T2, R> LazyDequeX<R> zip(BiFunction<? super T, ? super T2, ? extends R> fn,
            Publisher<? extends T2> publisher) {
       
        return (LazyDequeX<R>)super.zip(fn, publisher);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#fromStream(java.util.stream.Stream)
     */
    @Override
    public <X> LazyDequeX<X> fromStream(Stream<X> stream) {
        List<X> list = (List<X>) stream.collect((Collector)getCollector());
        return new LazyDequeX<X>(list, (Collector)getCollector());
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.ListX#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    public LazyDequeX<T> onEmptySwitch(Supplier<? extends Deque<T>> supplier) {
        if (this.isEmpty())
            return LazyDequeX.fromIterable(supplier.get());
        return this;
    }
    
    
}
