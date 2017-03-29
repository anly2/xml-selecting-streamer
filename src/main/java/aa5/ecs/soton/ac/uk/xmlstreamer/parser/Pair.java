package aa5.ecs.soton.ac.uk.xmlstreamer.parser;

public class Pair<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }


    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }
}
