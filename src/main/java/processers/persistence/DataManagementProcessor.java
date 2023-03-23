package processers.persistence;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import persistencelib.Atom;
import persistencelib.Key;
import persistencelib.StorageManager;
import persistencelib.Version;
import util.Quote;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * <p>Processor tasked with managing updates and access to the data used by the program.</p>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class DataManagementProcessor {

    private static final String
            ANSWER_ATOM = "ANSWER",
            REACTION_ATOM = "REACTION",
            IDENTIFIER_ATOM = "ID",
            QUOTE_ATOM = "QUOTE",
            JSON_ATOM = "JSON",
            MEMBER_ATOM = "MEMBER";

    public static final String PREFIX_ADMINISTRATORS = "admins";
    public static final String PREFIX_TEXT_CATEGORIES = "text-categories";
    public static final String PREFIX_TEXT_CHANNELS = "channels";
    public static final String PREFIX_HELPERS = "helpers";
    public static final String PREFIX_BLACKLISTED_USER = "blacklisted";
    public static final String PREFIX_ROLES = "roles";
    public static final String PREFIX_RESTORATOR_MEMBERS = "restorator";

    private final Set<Quote> quotes;
    private final Map<String, String> answers;
    private final Map<String, String> reactions;
    private final Map<String, String> identifiers;
    private final Map<String, JSONObject> objects;
    private final Map<String, Set<String>> members;

    private final StorageManager manager;

    public DataManagementProcessor(String dbName) throws IOException {
        this.quotes = new HashSet<>();
        this.answers = new HashMap<>();
        this.reactions = new HashMap<>();
        this.identifiers = new HashMap<>();
        this.objects = new HashMap<>();
        this.members = new HashMap<>();
        this.manager = new StorageManager(dbName, Version.V100);
    }

    private void initRegion(String regionIdentifier, BiConsumer<Atom, Key> action) {
        if (!manager.hasRegion(regionIdentifier)) manager.addRegion(regionIdentifier);
        Atom atom = manager.getRegion(regionIdentifier);
        if (atom != null) atom.getItems().forEach(k -> action.accept(atom, k));
    }

    public void initialize() {
        initRegion(IDENTIFIER_ATOM, (a, k) -> identifiers.put(k.toString(), a.getItem(k).iterator().next()));
        initRegion(ANSWER_ATOM, (a, k) -> identifiers.put(k.toString(), a.getItem(k).iterator().next()));
        initRegion(REACTION_ATOM, (a, k) -> identifiers.put(k.toString(), a.getItem(k).iterator().next()));
        initRegion(QUOTE_ATOM, (a, k) -> {
            List<String> l = new LinkedList<>(a.getItem(k));
            quotes.add(new Quote(l.get(0), l.get(1), l.get(2), OffsetDateTime.parse(l.get(3)), k.toString()));
        });
        JSONParser parser = new JSONParser();
        initRegion(JSON_ATOM, (a, k) -> {
            try {
                objects.put(k.toString(), (JSONObject) parser.parse(a.getItem(k).iterator().next()));
            } catch (ParseException e) {
                System.err.println("WARNING: Invalid JSON object found:\n" + a.getItem(k).iterator().next() + "\nTrace below:");
                e.printStackTrace();
            }
        });
        initRegion(MEMBER_ATOM, (a, k) -> members.put(k.toString(), new HashSet<>(a.getItem(k))));
    }

    public void save() throws IOException {
        manager.save();
    }

    // Answers
    public Set<String> getAnswerTriggers() {
        return Collections.unmodifiableSet(answers.keySet());
    }
    public String getAnswer(String trigger) {
        return answers.get(trigger);
    }

    /**
     * @return {@code true} if the answer wasn't previously in, {@code false} otherwise
     */
    public boolean addAnswer(String trigger, String answer) {
        if (!manager.addToRegion(ANSWER_ATOM, new Key(trigger), Collections.singleton(answer)))
            manager.replaceInRegion(ANSWER_ATOM, new Key(trigger), Collections.singleton(answer));
        return answers.put(trigger, answer) == null;
    }
    public boolean removeAnswer(String trigger) {
        boolean success1 = manager.removeFromRegion(ANSWER_ATOM, new Key(trigger));
        boolean success2 = answers.remove(trigger) != null;
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // Reactions
    public Set<String> getReactionTriggers() {
        return Collections.unmodifiableSet(reactions.keySet());
    }
    public String getReaction(String trigger) {
        return reactions.get(trigger);
    }
    /**
     * @return {@code true} if the reaction wasn't previously in, {@code false} otherwise
     */
    public boolean addReaction(String trigger, String reaction) {
        if (!manager.addToRegion(REACTION_ATOM, new Key(trigger), Collections.singleton(reaction)))
            manager.replaceInRegion(REACTION_ATOM, new Key(trigger), Collections.singleton(reaction));
        return reactions.put(trigger, reaction) == null;
    }
    public boolean removeReaction(String trigger) {
        boolean success1 = manager.removeFromRegion(REACTION_ATOM, new Key(trigger));
        boolean success2 = reactions.remove(trigger) != null;
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // Identifiers
    public Set<String> getIdentifierNames() {
        return Collections.unmodifiableSet(identifiers.keySet());
    }
    public String getIdentifier(String name) {
        return identifiers.get(name);
    }
    /**
     * @return {@code true} if the identifier wasn't previously in, {@code false} otherwise
     */
    public boolean addIdentifier(String name, String id) {
        if (!manager.addToRegion(IDENTIFIER_ATOM, new Key(name), Collections.singleton(id)))
            manager.replaceInRegion(IDENTIFIER_ATOM, new Key(name), Collections.singleton(id));
        return identifiers.put(name, id) == null;
    }
    public boolean removeIdentifier(String name) {
        boolean success1 = manager.removeFromRegion(IDENTIFIER_ATOM, new Key(name));
        boolean success2 = identifiers.remove(name) != null;
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // Quotes
    public Set<Quote> getQuotes() {
        return Collections.unmodifiableSet(quotes);
    }
    public Quote getQuote(String id) {
        for (Quote q : quotes) if (q.getId().equals(id)) return q;
        return null;
    }
    public Quote getRandomQuote(long seed) {
        Iterator<Quote> it = quotes.iterator();
        int i = new Random(seed).nextInt(quotes.size());
        for (;i != 0; i--) it.next();
        return it.next();
    }
    /**
     * @return {@code true} if the quote wasn't previously in, {@code false} otherwise
     */
    public boolean addQuote(Quote quote) {
        if (!manager.addToRegion(QUOTE_ATOM, new Key(quote.getId()), Arrays.asList(
                quote.getChannel(),
                quote.getAttachment(),
                quote.getNickname(),
                quote.getTime().toString())))
            manager.replaceInRegion(QUOTE_ATOM, new Key(quote.getId()), Arrays.asList(
                    quote.getChannel(),
                    quote.getAttachment(),
                    quote.getNickname(),
                    quote.getTime().toString()));
        return quotes.add(quote);
    }
    public boolean removeQuote(String id) {
        boolean success1 = manager.removeFromRegion(QUOTE_ATOM, new Key(id));
        boolean success2 = quotes.removeIf(q -> q.getId().equals(id));
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // JSON objects
    public Set<String> getJSONIdentifiers() {
        return Collections.unmodifiableSet(objects.keySet());
    }
    public JSONObject getJSONObject(String name) {
        return objects.get(name);
    }
    /**
     * @return {@code true} if the JSON object wasn't previously in, {@code false} otherwise
     */
    public boolean addJSONObject(String name, JSONObject object) {
        if (!manager.addToRegion(JSON_ATOM, new Key(name), Collections.singleton(object.toJSONString())))
            manager.replaceInRegion(JSON_ATOM, new Key(name), Collections.singleton(object.toJSONString()));
        return objects.put(name, object) == null;
    }
    public boolean removeJSONObject(String name) {
        boolean success1 = manager.removeFromRegion(JSON_ATOM, new Key(name));
        boolean success2 = objects.remove(name) != null;
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // Members
    public Set<String> getMemberNames() {
        return Collections.unmodifiableSet(members.keySet());
    }
    public Set<String> getMemberData(String name) {
        return members.get(name);
    }
    /**
     * @return {@code true} if the member wasn't previously in, {@code false} otherwise
     */
    public boolean addMember(String name) {
        if (!manager.addToRegion(MEMBER_ATOM, new Key(name), Collections.emptySet()))
            manager.replaceInRegion(MEMBER_ATOM, new Key(name), Collections.emptySet());
        return members.put(name, new HashSet<>()) == null;
    }

    /**
     * @return {@code true} if the member didn't already have the data and said data was added successfully,
     * {@code false} otherwise
     */
    public boolean addToMember(String name, String data) {
        Atom atom = manager.getRegion(MEMBER_ATOM);
        // Only update region if something actually changed
        if (atom.getItem(new Key(name)).add(data)) manager.replaceRegion(atom);
        return members.get(name).add(data);
    }
    public boolean removeMember(String name) {
        boolean success1 = manager.removeFromRegion(MEMBER_ATOM, new Key(name));
        boolean success2 = members.remove(name) != null;
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }
    public boolean removeFromMember(String name, String data) {
        Atom atom = manager.getRegion(MEMBER_ATOM);
        boolean success1 = atom.getItem(new Key(data)).remove(data);
        // Only update region if something actually changed
        if (success1) manager.replaceRegion(atom);
        boolean success2 = members.get(name).remove(data);
        // Return true if both are true, return false otherwise
        return success1 && success2;
    }

    // Identifier shortcuts
    public String getAdministrator(String name) {
        return getIdentifier(PREFIX_ADMINISTRATORS + "." + name);
    }
    public boolean addAdministrator(String name, String id) {
        return addIdentifier(PREFIX_ADMINISTRATORS + "." + name, id);
    }
    public boolean removeAdministrator(String name) {
        return removeIdentifier(PREFIX_ADMINISTRATORS + "." + name);
    }

    public String getTextCategory(String name) {
        return getIdentifier(PREFIX_TEXT_CATEGORIES + "." + name);
    }
    public boolean addTextCategory(String name, String id) {
        return addIdentifier(PREFIX_TEXT_CATEGORIES + "." + name, id);
    }
    public boolean removeTextCategory(String name) {
        return removeIdentifier(PREFIX_TEXT_CATEGORIES + "." + name);
    }

    public String getTextChannel(String name) {
        return getIdentifier(PREFIX_TEXT_CHANNELS + "." + name);
    }
    public boolean addTextChannel(String name, String id) {
        return addIdentifier(PREFIX_TEXT_CHANNELS + "." + name, id);
    }
    public boolean removeTextChannel(String name) {
        return removeIdentifier(PREFIX_TEXT_CHANNELS + "." + name);
    }

    public synchronized String getHelper(String name) {
        return getIdentifier(PREFIX_HELPERS + "." + name);
    }
    public synchronized boolean addHelper(String name, String id) {
        return addIdentifier(PREFIX_HELPERS + "." + name, id);
    }
    public boolean removeHelper(String name) {
        return removeIdentifier(PREFIX_HELPERS + "." + name);
    }

    public String getBlacklistedUser(String name) {
        return getIdentifier(PREFIX_BLACKLISTED_USER + "." + name);
    }
    public boolean addBlacklistedUser(String name, String id) {
        return addIdentifier(PREFIX_BLACKLISTED_USER + "." + name, id);
    }
    public boolean removeBlacklistedUser(String name) {
        return removeIdentifier(PREFIX_BLACKLISTED_USER + "." + name);
    }

    public String getRole(String name) {
        return getIdentifier(PREFIX_ROLES + "." + name);
    }
    public boolean addRole(String name, String id) {
        return addIdentifier(PREFIX_ROLES + "." + name, id);
    }
    public boolean removeRole(String name) {
        return removeIdentifier(PREFIX_ROLES + "." + name);
    }

    public Set<String> getRestoratorRoles(String memberID) {
        return members.get(PREFIX_RESTORATOR_MEMBERS + "." + memberID);
    }
    public boolean addRestoratorMember(String memberID) {
        return addMember(PREFIX_RESTORATOR_MEMBERS + "." + memberID);
    }
    public boolean removeRestoratorMember(String memberID) {
        return removeMember(PREFIX_RESTORATOR_MEMBERS + "." + memberID);
    }
    public boolean addToRestoratorMember(String memberID, String roleID) {
        return addToMember(PREFIX_RESTORATOR_MEMBERS + "." + memberID, roleID);
    }
    public boolean removeFromRestoratorMember(String memberID, String roleID) {
        return removeFromMember(PREFIX_RESTORATOR_MEMBERS + "." + memberID, roleID);
    }

    // Custom requests
    private Set<String> getIdsFromPrefix(String prefix) {
        return identifiers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }
    private Set<String> getNamesFromPrefix(String prefix) {
        return identifiers.keySet()
                .stream()
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.replace(prefix + ".", ""))
                .collect(Collectors.toSet());
    }
    private Set<String> getMembersFromPrefix(String prefix) {
        return getMemberNames()
                .stream()
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.replace(prefix + ".", ""))
                .collect(Collectors.toSet());
    }

    public String getBotOwner() {return "265904613687820288";}
    public Set<String> getBlacklistedUsers() {
        return getIdsFromPrefix(PREFIX_BLACKLISTED_USER);
    }
    public Set<String> getTextChannelCategories() {
        return getIdsFromPrefix(PREFIX_TEXT_CATEGORIES);
    }
    public Set<String> getAdministratorIDs() {
        return getIdsFromPrefix(PREFIX_ADMINISTRATORS);
    }
    public Set<String> getHelperIDs() {
        return getIdsFromPrefix(PREFIX_HELPERS);
    }
    public List<String> getSortedIdentifierNames() {
        return identifiers.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }
    public Set<String> getRestoratorMemberIDs() {
        return getMembersFromPrefix(PREFIX_RESTORATOR_MEMBERS);
    }
}
