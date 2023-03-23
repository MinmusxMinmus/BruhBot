package interfaces;

import util.Statics;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public interface ClownActions {

    List<Long> clowns = new LinkedList<>();
    List<String> names = new LinkedList<>();

    default void addClown(long id) {
        clowns.add(id);
    }

    default void addClown(String name) {
        names.add(name);
    }

    default boolean isClownId(long id) {
         return clowns.contains(id);
    }

    default boolean isClownName(String name) {
        return names.contains(name);
    }

    default boolean checkClown(long id, String name) {
        return isClownId(id) && isClownName(name);
    }

    default void removeClown(long id) {
        clowns.remove(id);
    }

    default void removeClown(String name) {
        names.remove(name);
    }

    default void removeAllClowns() {
        for (long id : clowns)
            clowns.remove(id);
        for (String name : names)
            names.remove(name);
    }

    default String getClownRole() {
        return Statics.CLOWN_ROLE_ID;
    }
}
