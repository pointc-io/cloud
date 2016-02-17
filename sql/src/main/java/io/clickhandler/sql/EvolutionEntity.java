package io.clickhandler.sql;


import java.util.Date;

/**
 *
 */
@Table(journal = false)
public class EvolutionEntity extends AbstractEntity {
    @Column
    private boolean success;
    @Column
    private Date end;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
}