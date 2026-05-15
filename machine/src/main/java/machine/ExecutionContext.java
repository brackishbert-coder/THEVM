package machine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Context passed to flatlanders during execution.
 *
 * SYNTHETIC CONTEXT: isSynthetic() == true indicates this context was
 * produced by ManifoldSpace.defaultContext() outside active execution.
 * Never substitute raw -1L for step values.
 */
public interface ExecutionContext {
    long getCurrentStep();
    ManifoldSpace getManifoldSpace();
    boolean isSynthetic();
    Map<String, Object> getContextAttributes();
}
