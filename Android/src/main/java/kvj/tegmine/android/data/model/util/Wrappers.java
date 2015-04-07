package kvj.tegmine.android.data.model.util;

/**
 * Created by kvorobyev on 4/8/15.
 */
public class Wrappers {
    public static class Pair<T> {

        private final T v1;
        private final T v2;

        public Pair(T v1, T v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        public T v1() {
            return v1;
        }

        public T v2() {
            return v2;
        }
    }

    public static class Tuple2<T1, T2> {

        private final T1 v1;
        private final T2 v2;

        public Tuple2(T1 v1, T2 v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        public T1 v1() {
            return v1;
        }

        public T2 v2() {
            return v2;
        }
    }

}
