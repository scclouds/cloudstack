package org.apache.cloudstack.secstorage.heuristics;

/**
 * The heuristic purpose used in the allocation process of secondary storage resources.
 * Valid options are: {@link #ISO}, {@link #SNAPSHOT}, {@link #TEMPLATE} and {@link #VOLUME}
 */
public enum HeuristicPurpose {
    ISO, SNAPSHOT, TEMPLATE, VOLUME
}
