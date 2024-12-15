import jakarta.persistence.*;
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team;
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "project")
@SQLDelete(sql = "UPDATE project SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "start_date", nullable = false)
    private Long startDate;

    @Column(name = "end_date", nullable = false)
    private Long endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Project parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Column(name = "external_task_id")
    private Long externalTaskId;

    @Column(name = "github_repo")
    private String githubRepo;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "deleted_at")
    private Long deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Version
    private Integer version;
}

interface ProjectRepository : JpaRepository<Project, IdType> {
    fun existsByName(name: String): Boolean
}