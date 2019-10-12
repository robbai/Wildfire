from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class qiwlwgdlirqeeqyk(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-3458.19,4312.86,348.0),
				velocity=Vector3(-757.72095,-1243.141,286.491),
				angular_velocity=Vector3(1.76491,1.18171,1.30081),
				rotation=Rotator(-0.024160197,1.8751956,-0.6473399))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-2956.47,4516.11,17.06),
						velocity=Vector3(-516.761,-294.841,0.211),
						angular_velocity=Vector3(-3.0999997E-4,0.0014099999,2.08301),
						rotation=Rotator(-0.016873788,-2.6419942,-9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=22.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-3426.95,4613.71,292.21),
						velocity=Vector3(-748.751,-741.631,491.07098),
						angular_velocity=Vector3(1.55901,-0.69091,-0.00731),
						rotation=Rotator(0.47524643,-2.3685622,1.5644686)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
