from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class bkysoqnxwfnnhvsh(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(2711.4,-2383.74,193.67),
				velocity=Vector3(-187.951,-212.311,531.21094),
				angular_velocity=Vector3(2.33981,-2.07071,2.17321),
				rotation=Rotator(-0.0618386,1.886317,1.0683217))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(3301.45,180.62,17.05),
						velocity=Vector3(-156.44101,88.010994,0.100999996),
						angular_velocity=Vector3(9.1E-4,-5.1E-4,-0.22990999),
						rotation=Rotator(-0.01639442,-0.56517607,9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0),
				1: CarState(
					physics=Physics(
						location=Vector3(3514.96,-4221.08,20.359999),
						velocity=Vector3(-174.841,721.8909,-95.601),
						angular_velocity=Vector3(0.93411,1.85401,2.50001),
						rotation=Rotator(-0.084848315,1.85372,0.08350608)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
