package hobo;

import java.util.*;

public class State implements Cloneable {
	public static final int NCARDS_PER_COLOR = 12,
	                        INITIAL_HAND_SIZE = 4,
	                        INITIAL_MISSION_COUNT = 3,
	                        OPEN_DECK_SIZE = 5;

	public final Random random = new Random(0);

	// the index into this array is used to refer to a player in many places here.
	private PlayerState[] players;
	private int current_player;

	public Map<Railway,Integer> owner_by_railway = new HashMap<Railway,Integer>();

	public CardBag deck      = new CardBag();
	public CardBag open_deck = new CardBag();
	public CardBag discarded = new CardBag();

	// deck of destination tickets
	public LinkedList<Mission> missions = new LinkedList<Mission>();

	private boolean game_over = false;
	private int last_player = -1;
	
	// for assigning colors to players
	private static final Color[] colors = new Color[]{ Color.BLUE, Color.RED, Color.GREEN,
	                                                   Color.YELLOW, Color.BLACK };
	private int next_color_index = 0;

	public State clone() {
		State that = new State();
		that.random.setSeed(this.random.getSeed());
		that.players = this.players.clone();
		for (int i = 0; i < players.length; i++)
			that.players[i] = that.players[i].clone();
		that.current_player = this.current_player;
		that.owner_by_railway.putAll(this.owner_by_railway);
		that.deck.addAll(this.deck);
		that.open_deck.addAll(this.open_deck);
		that.discarded.addAll(this.discarded);
		that.missions.addAll(this.missions);
		that.game_over = this.game_over;
		that.last_player = this.last_player;
		that.next_color_index = this.next_color_index;
		return that;
	}

	public String toString() {
		return "State(deck: "+deck+" open_deck: "+open_deck
		     + " missions: "+missions+" last_player: "+last_player+")";
	}

	public State() {}
	
	public State(String... player_names) {
		int ni = player_names.length;
		players = new PlayerState[ni];
		for (int i = 0; i < ni; i++)
			players[i] = new PlayerState(i, player_names[i], colors[next_color_index++]);
		current_player = 0;
	}

	public void setup() {
		for (Color c: Color.values())
			deck.addAll(Collections.nCopies(NCARDS_PER_COLOR, c));
		// two more grey cards than other colors
		deck.add(Color.GREY); deck.add(Color.GREY);

		missions.addAll(Mission.missions);
		Collections.shuffle(missions, new java.util.Random(random.getSeed()));

		for (PlayerState p: players) {
			for (int i = 0; i < INITIAL_HAND_SIZE; i++)
				p.hand.add(deck.draw(random));
			// XXX: officially, players can choose to discard one
			// of the missions dealt.
			for (int i = 0; i < INITIAL_MISSION_COUNT; i++)
				p.missions.add(missions.removeFirst());
		}

		for (int i = 0; i < OPEN_DECK_SIZE; i++)
			open_deck.add(deck.draw(random));
	}

	public void switchTurns() {
		if (currentPlayerState().almostOutOfCars()) {
			if (last_player < 0) {
				last_player = current_player;
			} else if (last_player == current_player) {
				game_over = true;
				return;
			}
		}

		current_player++;
		current_player %= players.length;
	}

	// this results in invalid states (used in best-reply search)
	public void switchToPlayer(int handle) {
		current_player = handle;
	}

	public int[] players() {
		int[] handles = new int[players.length];
		for (int i = 0; i < handles.length; i++)
			handles[i] = i;
		return handles;
	}
	
	public List<PlayerState> playerStates() {
		List<PlayerState> playerStates = new ArrayList<PlayerState>();
		for (int i = 0; i < players.length; i++)
			playerStates.add(players[i]);
		return playerStates;
	}
	
	public int currentPlayer() {
		return current_player;
	}

	public PlayerState currentPlayerState() {
		return playerState(current_player);
	}

	public PlayerState playerState(int handle) {
		return players[handle];
	}

	public boolean gameOver() {
		return game_over;
	}

	public boolean isDraw() {
		int winner = winner();
		for (int handle: players()) {
			if (handle != winner && players[winner].finalScore() == players[handle].finalScore())
				return true;
		}
		return false;
	}

	public int winner() {
		int xmax = Integer.MIN_VALUE;
		int winner = -1;
		for (int i = 0; i < players.length; i++) {
			int x = players[i].finalScore();
			if (x > xmax) {
				xmax = x;
				winner = i;
			}
		}
		return winner;
	}

	public boolean isClaimed(Railway r) {
		return owner_by_railway.containsKey(r);
	}

	public CardBag openCards() {
		return open_deck;
	}
	
	public void restoreDecks() {
		if (deck.isEmpty()) {
			deck = discarded;
			discarded = new CardBag();
		}
		while (open_deck.size() < OPEN_DECK_SIZE && !deck.isEmpty())
			open_deck.add(deck.draw(random));
	}
	
	public void applyDecision(Decision d) throws IllegalDecisionException {
		d.requireLegal(this);
		d.apply(this);
	}

	public Set<Decision> allPossibleDecisions() {
		return allPossibleDecisionsFor(current_player);
	}
	
	public Set<Decision> allPossibleDecisionsFor(int handle) {
		PlayerState ps = players[handle];
		Set<Decision> ds = new LinkedHashSet<Decision>(100);

		if (ps.drawn_missions == null) {
			if (ps.drawn_card == null) {
				// claim
				for (Railway r: Railway.railways) {
					if (!isClaimed(r) && r.length <= ps.ncars && !ps.railways.contains(r.dual)) {
						for (Color c: Color.values()) {
							CardBag cs = ps.hand.cardsToClaim(r, c);
							if (cs != null)
								ds.add(new ClaimRailwayDecision(r, cs));
						}
					}
				}
			}
			
			for (Color c: Color.values())
				if (open_deck.contains(c))
					ds.add(new DrawCardDecision(c));

			if (!deck.isEmpty())
				ds.add(new DrawCardDecision(null));
			
			if (ps.drawn_card == null) {
				ds.add(new DrawMissionsDecision());
			}
		} else {
			// keep
			for (Set<Mission> ms: Util.powerset(ps.drawn_missions)) {
				if (ms.isEmpty())
					continue;
				ds.add(new KeepMissionsDecision(ms));
			}
		}

		return ds;
	}
}
