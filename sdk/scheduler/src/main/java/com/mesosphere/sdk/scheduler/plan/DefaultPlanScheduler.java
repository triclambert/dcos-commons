package com.mesosphere.sdk.scheduler.plan;

import com.google.inject.Inject;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.SchedulerDriver;
import com.mesosphere.sdk.offer.*;
import com.mesosphere.sdk.scheduler.TaskKiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default deployment scheduler. See docs in {@link PlanScheduler} interface.
 */
public class DefaultPlanScheduler implements PlanScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlanScheduler.class);

    private final OfferAccepter offerAccepter;
    private final OfferEvaluator offerEvaluator;
    private final TaskKiller taskKiller;

    @Inject
    public DefaultPlanScheduler(OfferAccepter offerAccepter, OfferEvaluator offerEvaluator, TaskKiller taskKiller) {
        this.offerAccepter = offerAccepter;
        this.offerEvaluator = offerEvaluator;
        this.taskKiller = taskKiller;
    }

    @Override
    public Collection<OfferID> resourceOffers(
            final SchedulerDriver driver,
            final List<Offer> offers,
            final Collection<? extends Step> steps) {
        if (driver == null || offers == null || steps == null) {
            logger.error("Unexpected null argument(s) encountered: driver='{}' offers='{}', steps='{}'",
                    driver, offers, steps);
            return Collections.emptyList();
        }

        List<OfferID> acceptedOfferIds = new ArrayList<>();
        List<Offer> availableOffers = new ArrayList<>(offers);

        for (Step step : steps) {
            acceptedOfferIds.addAll(resourceOffers(driver, availableOffers, step));
            availableOffers = PlanUtils.filterAcceptedOffers(availableOffers, acceptedOfferIds);
        }

        return acceptedOfferIds;
    }

    private Collection<OfferID> resourceOffers(
            SchedulerDriver driver,
            List<Offer> offers,
            Step step) {

        if (!step.isPending()) {
            logger.info("Ignoring resource offers for step: {} status: {}", step.getName(), step.getStatus());
            return Collections.emptyList();
        }

        logger.info("Processing resource offers for step: {}", step.getName());
        Optional<OfferRequirement> offerRequirementOptional = step.start();
        if (!offerRequirementOptional.isPresent()) {
            logger.info("No OfferRequirement for step: {}", step.getName());
            step.updateOfferStatus(Collections.emptyList());
            return Collections.emptyList();
        }

        OfferRequirement offerRequirement = offerRequirementOptional.get();
        // It is harmless to attempt to kill tasks which have never been launched.  This call attempts to Kill all Tasks
        // with a Task name which is equivalent to that expressed by the OfferRequirement.  If no such Task is currently
        // running no operation occurs.
        killTasks(offerRequirement);

        // Step has returned an OfferRequirement to process. Find offers which match the
        // requirement and accept them, if any are found:
        List<OfferRecommendation> recommendations = offerEvaluator.evaluate(offerRequirement, offers);
        if (recommendations.isEmpty()) {
            // Log that we're not finding suitable offers, possibly due to insufficient resources.
            logger.warn(
                    "Unable to find any offers which fulfill requirement provided by step {}: {}",
                    step.getName(), offerRequirement);
            step.updateOfferStatus(Collections.emptyList());
            return Collections.emptyList();
        }

        List<OfferID> acceptedOffers = offerAccepter.accept(driver, recommendations);
        // Notify step of offer outcome:
        if (acceptedOffers.isEmpty()) {
            // If no Operations occurred it may be of interest to the Step.  For example it may want to set its state
            // to Pending to ensure it will be reattempted on the next Offer cycle.
            step.updateOfferStatus(Collections.emptyList());
        } else {
            step.updateOfferStatus(getOperations(recommendations));
        }

        return acceptedOffers;
    }

    private void killTasks(OfferRequirement offerRequirement) {
        for (TaskRequirement taskRequirement : offerRequirement.getTaskRequirements()) {
            String taskName = taskRequirement.getTaskInfo().getName();
            taskKiller.killTask(taskName, false);
        }
    }

    private static Collection<Offer.Operation> getOperations(Collection<OfferRecommendation> recommendations) {
        return recommendations.stream()
                .map(OfferRecommendation::getOperation)
                .collect(Collectors.toList());
    }
}
