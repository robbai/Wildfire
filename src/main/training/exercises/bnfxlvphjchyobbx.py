from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class bnfxlvphjchyobbx(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-2169.28,1420.73,148.22),
				velocity=Vector3(497.82098,612.811,19.771),
				angular_velocity=Vector3(2.09041,3.24801,1.06371),
				rotation=Rotator(0.28886777,-0.10594055,0.28963473))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-469.87,4299.6597,17.05),
						velocity=Vector3(330.311,179.011,0.29099998),
						angular_velocity=Vector3(4.0999998E-4,3.0999997E-4,1.81161),
						rotation=Rotator(-0.016969662,-2.3932018,-9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=25.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-2152.53,1369.25,16.64),
						velocity=Vector3(495.291,633.751,4.561),
						angular_velocity=Vector3(-0.60511,-0.46851,2.2571099),
						rotation=Rotator(-0.021571605,0.94138485,0.04084224)),
					jumped=False,
					double_jumped=False,
					boost_amount=1.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
