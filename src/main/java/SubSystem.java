public interface SubSystem {
    default void exit(int status) {
        System.exit(status);
    }
}
