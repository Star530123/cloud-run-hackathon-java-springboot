package hello;

import java.util.List;
import java.util.Map;

/**
 * @author StarL
 */
public class Request {

    public Links _links;
    public Arena arena;

    static class Arena {
        public List<Integer> dims;
        public Map<String, PlayerState> state;
    }

    static class Self {
        public String href;
    }

    static class Links {
        public Self self;
    }

    static class PlayerState {
        public Integer x;
        public Integer y;
        public String direction;
        public Boolean wasHit;
        public Integer score;
        public Service.Direction d;
    }
}
