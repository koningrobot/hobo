package hobo;

public class DrawCardDecision extends Decision {
	// if color is not null, draw from open deck
	public final Color color;
	public DrawCardDecision(int player) {
		this.player = player;
		this.color = null;
	}
	public DrawCardDecision(int player, Color color) {
		this.player = player;
		this.color = color;
	}

	@Override public String toString() {
		return "DrawCardDecision(player: "+player+" color: "+color+")";
	}
	
	@Override public boolean equals(Object o) {
		if (!(o instanceof DrawCardDecision))
			return false;
		DrawCardDecision that = (DrawCardDecision)o;
		return that.player == this.player && that.color == this.color;
	}

	private static final int classHashCode = "DrawCardDecision".hashCode();
	@Override public int hashCode() {
		return player ^ (color == null ? -1 : color.hashCode()) ^ classHashCode;
	}

	@Override public String reasonForIllegality(State s) {
		if (s.currentPlayer() != player)
			return "it's not your turn";
		
		PlayerState p = s.playerState(player);

		if (p.drawn_missions != null)
			return "you drew mission cards and now must decide which to keep";

		if (color == null) {
			if (s.deck.isEmpty())
				return "deck is empty";
		} else {
			if (!s.open_deck.contains(color))
				return "no such card in the open deck";
		}
		return null;
	}
	
	@Override public void apply(State s) {
		s.switchToPlayer(player);
		PlayerState p = s.playerState(player);

		// if drew a card last time, then can draw one more
		boolean last_draw = p.drawn_card != null;

		if (color == null) {
			p.drawn_card = s.deck.draw(s.random);
		} else {
			p.drawn_card = s.open_deck.draw(color);
			if (p.drawn_card == Color.GREY)
				last_draw = true;
		}

		p.hand.add(p.drawn_card);
		
		s.restoreDecks();

		if (last_draw) {
			p.drawn_card = null;
			s.switchTurns();
		}
	}
}
