package com.mawa3id.donation;

public enum DonationStatus {
    /** A checkout session has been created; awaiting payment confirmation. */
    PENDING,
    /** Payment completed successfully (confirmed via provider webhook). */
    SUCCEEDED,
    /** Payment attempt failed. */
    FAILED,
    /** The checkout session expired before payment completed. */
    EXPIRED
}
