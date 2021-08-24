from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class lcahjrbokepkrqoe(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-2249.94,403.27,93.14),
				velocity=Vector3(235.141,-415.121,0.0),
				angular_velocity=Vector3(4.5493097,2.57681,-2.55091),
				rotation=Rotator(0.094723314,1.2544128,-0.2802391))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-129.4,5240.78,17.02),
						velocity=Vector3(96.010994,-479.211,0.151),
						angular_velocity=Vector3(3.0999997E-4,-8.1E-4,-2.33901),
						rotation=Rotator(-0.009491506,-1.5245851,9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=99.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-1682.64,-4367.05,17.06),
						velocity=Vector3(-1213.4509,238.991,0.121),
						angular_velocity=Vector3(0.0,-4.0999998E-4,0.010609999),
						rotation=Rotator(-0.016682042,2.948311,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
