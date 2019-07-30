package grakn.core.graql.reasoner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Profiler {

    private final Map<String, Long> registeredTimes = new HashMap<>();
    private final Map<String, Long> registeredCalls = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(Profiler.class);

    public void updateTime(String name, long increment){
        Long match = registeredTimes.get(name);
        registeredTimes.put(name, match != null? (match + increment) : increment);
    }

    public void updateCallCount(String name){
        Long match = registeredCalls.get(name);
        registeredCalls.put(name, match != null? (match + 1) : 1);
    }

    public long getTime(String name){ return registeredTimes.get(name);}
    public long getCount(String name){ return registeredCalls.get(name);}

    public void print(){
        registeredTimes.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(System.out::println);
        System.out.println();

        registeredCalls.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(System.out::println);
        System.out.println();
    }

    private static long cutOff = 100;

    public void logTimes(){
        registeredTimes.entrySet().stream()
                .filter(e -> e.getValue() > cutOff)
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(e -> LOG.debug(e.toString()));
        LOG.debug("");

        registeredCalls.entrySet().stream()
                .filter(e -> e.getValue() > cutOff)
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(e -> LOG.debug(e.toString()));
        clear();

    }

    public void clear(){
        registeredTimes.clear();
        registeredCalls.clear();
    }
}
