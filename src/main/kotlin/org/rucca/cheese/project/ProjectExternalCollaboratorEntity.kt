import jakarta.persistence.*
import org.rucca.cheese.user.User

@Entity
@Table(
    name = "project_external_collaborator",
    uniqueConstraints = UniqueConstraint(columnNames = ["project_id", "user_id"])
)
class ProjectExternalCollaborator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private val project: Project? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private val user: User? = null

    @Column(name = "created_at", nullable = false)
    private val createdAt: Long? = null
}