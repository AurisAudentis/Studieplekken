package be.ugent.blok2.daos;

import be.ugent.blok2.helpers.Language;
import be.ugent.blok2.helpers.exceptions.AlreadyExistsException;
import be.ugent.blok2.helpers.exceptions.NoSuchPenaltyEventException;
import be.ugent.blok2.helpers.exceptions.NoSuchUserException;
import be.ugent.blok2.model.penalty.Penalty;
import be.ugent.blok2.model.penalty.PenaltyEvent;

import java.util.List;

public interface IPenaltyEventsDao {
    List<PenaltyEvent> getPenaltyEvents();

    /**
     * Get the PenaltyEvent associated with the given code.
     */
    PenaltyEvent getPenaltyEvent(int code) throws NoSuchPenaltyEventException;

    /**
     * Get the actual occurrence of a PenaltyEvent: e.g. someone has cancelled after 17:00 the day before opening
     */
    List<Penalty> getPenalties(String augentId) throws NoSuchUserException;

    /**
     * addPenaltyEvent() adds an entry in the data structure which holds the PenaltyEvents.
     * The code which identifies the PenaltyEvent is received through the object itself (event.getCode()).
     */
    void addPenaltyEvent(PenaltyEvent event) throws AlreadyExistsException;

    /**
     * addDescription() adds a description associated with the code.
     * If, in the data layer, there is no code found equal to the parameter, an entry should be created.
     * The same counts for the language: if there is no language associated with the given code, an entry should be created.
     */
    void addDescription(int code, Language language, String description) throws NoSuchPenaltyEventException, AlreadyExistsException;

    /**
     * addPenalty() adds a Penalty to the so called Penalty Book (like the Order Book in a stock exchange market)
     */
    void addPenalty(Penalty penalty) throws NoSuchUserException, NoSuchPenaltyEventException;

    /**
     * updatePenaltyEvent() is meant to update an existing PenaltyEvent and/or description(s) associated with the given code.
     * If there is no PenaltyEvent associated with the given code in the first place, nothing should happen.
     * Note: it is assumed that code == event.getCode(), this is a precondition
     */
    void updatePenaltyEvent(int code, PenaltyEvent event) throws NoSuchPenaltyEventException;

    /**
     * updatePenalty() removes the Penalties 'remove' and adds the Penalties 'add' for the user identified by augentID
     */
    void updatePenalties(String augentID, List<Penalty> remove, List<Penalty> add);

    /**
     * deletePenaltyEvent() deletes an existing PenaltyEvent.
     * When a PenaltyEvent gets deleted, it should invoke a cascade of deletions for the corresponding descriptions.
     */
    void deletePenaltyEvent(int code) throws NoSuchPenaltyEventException;

    /**
     * deleteDescription() deletes a description for given PenaltyEvent in given language
     */
    void deleteDescription(int code, Language language) throws NoSuchPenaltyEventException;

    /**
     * deleteEvent() deletes a Penalty
     */
    void deletePenalty(Penalty penalty) throws NoSuchUserException, NoSuchPenaltyEventException;
}
