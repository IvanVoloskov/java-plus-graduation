package ewm.stats.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_actions")
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UserActionId id;
    @Column(nullable = false)
    private double weight;
    @Column(name = "action_date", nullable = false)
    private Instant actionDate;
}
