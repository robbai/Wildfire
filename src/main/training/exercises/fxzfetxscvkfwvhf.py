from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class fxzfetxscvkfwvhf(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-3895.27,3227.5898,201.58),
				velocity=Vector3(423.391,1219.121,359.031),
				angular_velocity=Vector3(3.07531,3.0401099,-4.15931),
				rotation=Rotator(0.12137623,-0.37659228,1.676641))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-358.28,4831.12,16.72),
						velocity=Vector3(-124.821,-101.711,6.961),
						angular_velocity=Vector3(-0.00291,-2.1E-4,-0.63901),
						rotation=Rotator(-0.017353158,-2.4077747,-1.917476E-4)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-3983.5798,2995.68,67.95),
						velocity=Vector3(-231.841,712.42096,193.391),
						angular_velocity=Vector3(-1.54851,1.24611,-2.0271099),
						rotation=Rotator(0.2554078,1.9238995,-0.6038132)),
					jumped=False,
					double_jumped=False,
					boost_amount=69.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
