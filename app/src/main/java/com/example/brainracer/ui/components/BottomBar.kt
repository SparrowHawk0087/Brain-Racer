import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.brainracer.R

fun bottomBarSelectedKey(route: String): String = when {
    route.startsWith("home/")        -> "home"
    route.startsWith("leaderboard/") -> "leaderboard"
    route.startsWith("challenges/")  -> "challenges"
    route == "quizzes"               -> "quizzes"
    route.startsWith("profile/")     -> "profile"
    else                             -> ""
}

enum class BottomBarGlassIntensity {
    Medium,
    Strong
}