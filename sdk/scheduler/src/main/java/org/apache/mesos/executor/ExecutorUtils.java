package org.apache.mesos.executor;

import org.apache.mesos.Protos;
import org.apache.mesos.specification.RLimitSpec;
import org.apache.mesos.specification.util.RLimit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Various utility methods for manipulating data in {@link org.apache.mesos.Protos.ExecutorInfo}s.
 */
public class ExecutorUtils {
    private static final String EXECUTOR_NAME_DELIM = "__";
    private static final Map<String, Protos.RLimitInfo.RLimit.Type> RLIMIT_TYPE_MAP = new HashMap<>();

    static {
        RLIMIT_TYPE_MAP.put(RLimit.AS, Protos.RLimitInfo.RLimit.Type.RLMT_AS);
        RLIMIT_TYPE_MAP.put(RLimit.CORE, Protos.RLimitInfo.RLimit.Type.RLMT_CORE);
        RLIMIT_TYPE_MAP.put(RLimit.CPU, Protos.RLimitInfo.RLimit.Type.RLMT_CPU);
        RLIMIT_TYPE_MAP.put(RLimit.DATA, Protos.RLimitInfo.RLimit.Type.RLMT_DATA);
        RLIMIT_TYPE_MAP.put(RLimit.FSIZE, Protos.RLimitInfo.RLimit.Type.RLMT_FSIZE);
        RLIMIT_TYPE_MAP.put(RLimit.LOCKS, Protos.RLimitInfo.RLimit.Type.RLMT_LOCKS);
        RLIMIT_TYPE_MAP.put(RLimit.MEMLOCK, Protos.RLimitInfo.RLimit.Type.RLMT_MEMLOCK);
        RLIMIT_TYPE_MAP.put(RLimit.MSGQUEUE, Protos.RLimitInfo.RLimit.Type.RLMT_MSGQUEUE);
        RLIMIT_TYPE_MAP.put(RLimit.NICE, Protos.RLimitInfo.RLimit.Type.RLMT_NICE);
        RLIMIT_TYPE_MAP.put(RLimit.NOFILE, Protos.RLimitInfo.RLimit.Type.RLMT_NOFILE);
        RLIMIT_TYPE_MAP.put(RLimit.NPROC, Protos.RLimitInfo.RLimit.Type.RLMT_NPROC);
        RLIMIT_TYPE_MAP.put(RLimit.RSS, Protos.RLimitInfo.RLimit.Type.RLMT_RSS);
        RLIMIT_TYPE_MAP.put(RLimit.RTPRIO, Protos.RLimitInfo.RLimit.Type.RLMT_RTPRIO);
        RLIMIT_TYPE_MAP.put(RLimit.RTTIME, Protos.RLimitInfo.RLimit.Type.RLMT_RTTIME);
        RLIMIT_TYPE_MAP.put(RLimit.SIGPENDING, Protos.RLimitInfo.RLimit.Type.RLMT_SIGPENDING);
        RLIMIT_TYPE_MAP.put(RLimit.STACK, Protos.RLimitInfo.RLimit.Type.RLMT_STACK);
    }

    private ExecutorUtils() {
        // do not instantiate
    }

    /**
     * Converts the unique {@link Protos.ExecutorID} into a Framework defined executor name.
     *
     * For example: "instance-0_aoeu5678" => "instance-0"
     */
    public static String toExecutorName(Protos.ExecutorID executorId) throws ExecutorTaskException {
      int underScoreIndex = executorId.getValue().lastIndexOf(EXECUTOR_NAME_DELIM);

      if (underScoreIndex == -1) {
        throw new ExecutorTaskException(String.format(
                "ExecutorID '%s' is malformed.  Expected '%s' to extract ExecutorName from ExecutorID.  "
                + "ExecutorIDs should be generated with ExecutorUtils.toExecutorId().",
                executorId, EXECUTOR_NAME_DELIM));
      }

      return executorId.getValue().substring(0, underScoreIndex);
    }

    /**
     * Converts the Framework defined Executor name into a unique {@link Protos.ExecutorID}.
     *
     * For example: "instance-0" => "instance-0_aoeu5678"
     */
    public static Protos.ExecutorID toExecutorId(String executorName) {
        return Protos.ExecutorID.newBuilder()
                .setValue(executorName + EXECUTOR_NAME_DELIM + UUID.randomUUID())
                .build();
    }

    public static Protos.RLimitInfo getRLimitInfo(RLimitSpec rLimitSpec) {
        Protos.RLimitInfo.Builder rLimitInfoBuilder = Protos.RLimitInfo.newBuilder();

        for (RLimit rLimit : rLimitSpec.getRLimits()) {
            Optional<Long> soft = rLimit.getSoft();
            Optional<Long> hard = rLimit.getHard();
            Protos.RLimitInfo.RLimit.Builder rLimitsBuilder = Protos.RLimitInfo.RLimit.newBuilder()
                    .setType(RLIMIT_TYPE_MAP.get(rLimit.getName()));

            if (soft.isPresent() && hard.isPresent()) {
                rLimitsBuilder.setSoft(soft.get()).setHard(hard.get());
            }
            rLimitInfoBuilder.addRlimits(rLimitsBuilder);
        }

        return rLimitInfoBuilder.build();
    }
}
